package nif.gui.util;

import nif.j3d.animation.J3dNiControllerManager;

public class ControllerInvokerThread extends Thread
{
	private J3dNiControllerManager cont;

	private J3dNiControllerManager optionalCont;

	public ControllerInvokerThread(String name, J3dNiControllerManager cont, J3dNiControllerManager optionalCont)
	{

		this.setDaemon(true);
		this.setName("ControllerInvokerThread " + name);

		this.cont = cont;
		this.optionalCont = optionalCont;
	}

	@Override
	public void run()
	{
		try
		{
			Thread.sleep(1000);

			String[] actions = cont.getAllSequences();
			while (cont.isLive())
			{
				for (int i = 0; i < actions.length; i++)
				{
					Thread.sleep((long) (Math.random() * 3000) + 1000);
					System.out.println("firing " + actions[i]);

					cont.getSequence(actions[i]).fireSequenceOnce();

					if (optionalCont != null)
						optionalCont.getSequence(actions[i]).fireSequenceOnce();

					Thread.sleep(cont.getSequence(actions[i]).getLengthMS());
				}
			}
		}
		catch (InterruptedException e)
		{
		}
	}

}
